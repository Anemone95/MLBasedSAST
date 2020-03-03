import json
import logging
import os

from django.shortcuts import render
from django.http import JsonResponse

from ml_app import tasks
from ml_app.models import *

# Create your views here.
import django_server.settings
from _pytorch import ctrl as blstm
import _settings

MODEL = None
MODEL_TIME = None


def get_model():
    global MODEL
    global MODEL_TIME
    ml_model = MLModel.objects.all().order_by('-create_time')[0]
    if MODEL and ml_model.create_time == MODEL_TIME:
        return MODEL
    else:
        ml_model = MLModel.objects.all().order_by('-create_time')[0]
        if not ml_model:
            return None
        MODEL = blstm.load(ml_model.token_dict_path,
                           ml_model.model_path,
                           ml_model.config.word_freq_gt,
                           ml_model.config.embedding_dim,
                           ml_model.config.hidden_dim)
        MODEL_TIME=ml_model.create_time
        return MODEL


def check_token(func):
    def handle(*args, **kwargs):
        request = args[0]
        token = request.GET.get('token', None)
        if token is None:
            return JsonResponse({"msg": "no token param"}, status=403)
        find_num = ClientToken.objects.filter(token=token).count()
        if find_num == 0:
            return JsonResponse({"msg": "token invalid"}, status=403)
        else:
            return func(*args, **kwargs)

    return handle


@check_token
def verify(request):
    return JsonResponse({"msg": "alive"}, status=200)


def train(request):
    config_name = request.GET.get('config', None)
    if config_name is None:
        return JsonResponse({"msg": "no config param"}, status=403)
    if not ModelConfig.objects.filter(name=config_name).exists():
        return JsonResponse({"msg": "no config named " + config_name}, status=403)
    res = tasks.blstm_train.delay(config_name)
    return JsonResponse({'msg': 'Training', 'task_id': res.task_id}, status=202)


def predict(request):
    slice_json = json.loads(request.body)
    project = Project.objects.get_or_create(name=slice_json["project"])[0]

    # 有label直接返回
    find_flow = TaintFlow.objects.filter(hash=slice_json["flowHash"])
    if len(find_flow) != 0:
        label = Label.objects.filter(taint_flow=find_flow[0])
        if len(label) != 0:
            print({"msg": str(label[0].is_safe)})
            return JsonResponse({"msg": str(label[0].is_safe)})

    # 无label先上传slice，再根据slice做预测
    if not get_model():
        logging.warning("Model not generalized, please train first")
        return JsonResponse({"msg": "Model not generalized, please train first"}, status=500)

    data_dir = _settings.relative_path_from_root(
        'data/slice/' + slice_json['project'] + "/" + slice_json["flowHash"][:2])
    if not os.path.exists(data_dir):
        os.makedirs(data_dir)
    slice_file_name = data_dir + '/slice-' + slice_json["flowHash"] + ".json"
    with open(slice_file_name, 'w') as f:
        json.dump(slice_json, f)

    taint_flows_num = TaintFlow.objects.filter(hash=slice_json["flowHash"]).count()
    if taint_flows_num == 0:
        taint_flow = TaintFlow.objects.create(hash=slice_json["flowHash"],
                                              slice_file=slice_file_name,
                                              project=project)

        if "flow" in slice_json:
            entry = MethodDescription.objects.get_or_create(clazz=slice_json["flow"]["entry"]["clazz"],
                                                            method=slice_json["flow"]["entry"]["method"],
                                                            sig=slice_json["flow"]["entry"]["sig"],
                                                            project=project)[0]
            location = Location.objects.get_or_create(source_file=slice_json["flow"]["point"]["sourceFile"],
                                                      start_line=slice_json["flow"]["point"]["startLine"],
                                                      end_line=slice_json["flow"]["point"]["endLine"],
                                                      project=project)[0]
            taint_flow.entry = entry
            taint_flow.point = location
        taint_flow.save()

    isTP = blstm.predict(MODEL[0], MODEL[1], slice_json["slice"])
    logging.info("Predict {0} as {1}".format(slice_json["flowHash"], isTP))
    return JsonResponse({"msg": str(isTP)})


def label(request):
    label_json = json.loads(request.body)
    taint_flow = TaintFlow.objects.get(hash=label_json["flowHash"])
    labels = Label.objects.filter(taint_flow=taint_flow)
    if len(labels) == 0:
        label = Label.objects.create(taint_flow=taint_flow)
        label.is_safe = label_json["isSafe"]
        label.save()

        data_dir = _settings.relative_path_from_root('data/label/' + taint_flow.project.name
                                                     + "/" + label_json["flowHash"][:2])
        if not os.path.exists(data_dir):
            os.makedirs(data_dir)

        with open(data_dir + "/label-" + label_json["flowHash"] + ".json", 'w') as f:
            json.dump(label_json, f)
    else:
        label = labels[0]
        label.is_safe = label_json["isSafe"]
        label.save()
        data_dir = _settings.relative_path_from_root('data/label/' + label.taint_flow.project.name
                                                     + "/" + label_json["flowHash"][:2])
        if not os.path.exists(data_dir):
            os.makedirs(data_dir)

        with open(data_dir + "/label-" + label_json["flowHash"] + ".json", 'w') as f:
            json.dump(label_json, f)

    return JsonResponse({"msg": "ok"})
