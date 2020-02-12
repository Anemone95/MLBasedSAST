import os

from django.db import models
from django.db.models.signals import post_delete
from django.dispatch import receiver


class ClientToken(models.Model):
    """
    用来发给客户端，防止恶意用户伪造垃圾数据影响使用
    """

    class Meta:
        app_label = 'ml_app'

    def __str__(self):
        return self.token

    token = models.CharField(max_length=255, unique=True)
    description = models.CharField(max_length=255, null=True)
    create_time = models.DateTimeField(auto_now_add=True)


# Create your models here.
class Project(models.Model):
    class Meta:
        app_label = 'ml_app'

    def __str__(self):
        return "{0}, created at {1}".format(self.name, self.create_time.strftime("%Y-%m-%d, %H:%M:%S"))

    name = models.CharField(max_length=255)
    create_time = models.DateTimeField(auto_now_add=True)


class MethodDescription(models.Model):
    class Meta:
        app_label = 'ml_app'
        unique_together = ('clazz', 'method', "sig", "project")

    def __str__(self):
        return "{0}#{1}#{2}".format(self.clazz, self.method, self.sig)

    clazz = models.CharField(max_length=255)
    method = models.CharField(max_length=255)
    sig = models.CharField(max_length=255)
    project = models.ForeignKey(Project, on_delete=models.CASCADE)
    create_time = models.DateTimeField(auto_now_add=True)


class Location(models.Model):
    class Meta:
        app_label = 'ml_app'
        unique_together = ('source_file', 'start_line', "end_line", "project")

    def __str__(self):
        return "{0},Line:{1}-{2}".format(self.source_file, self.start_line, self.end_line)

    source_file = models.CharField(max_length=255)
    start_line = models.PositiveIntegerField()
    end_line = models.PositiveIntegerField()
    project = models.ForeignKey(Project, on_delete=models.CASCADE)
    create_time = models.DateTimeField(auto_now_add=True)


class TaintFlow(models.Model):

    def __str__(self):
        return "{0}-{1}".format(self.project, self.hash)

    hash = models.CharField(max_length=255, unique=True)
    slice_file = models.CharField(max_length=255)
    project = models.ForeignKey(Project, on_delete=models.CASCADE)

    entry = models.ForeignKey(MethodDescription, on_delete=models.CASCADE, null=True)
    point = models.ForeignKey(Location, on_delete=models.CASCADE, null=True)
    create_time = models.DateTimeField(auto_now_add=True)


class Label(models.Model):
    class Meta:
        app_label = 'ml_app'

    def __str__(self):
        return "{0}: {1}".format(self.taint_edge, self.is_safe)

    taint_flow = models.OneToOneField(TaintFlow, on_delete=models.CASCADE)
    is_safe = models.BooleanField(default=False)
    create_time = models.DateTimeField(auto_now_add=True)
    update_time = models.DateTimeField(auto_now=True)


class ModelConfig(models.Model):
    class Meta:
        app_label = 'ml_app'

    def __str__(self):
        return "Config: {0}".format(self.name)

    name = models.CharField(max_length=255, primary_key=True)
    slice_dir = models.CharField(max_length=255)
    label_dir = models.CharField(max_length=255)
    embedding_dim = models.PositiveIntegerField()
    hidden_dim = models.PositiveIntegerField()
    word_freq_gt = models.PositiveIntegerField()
    early_stop_patience = models.PositiveIntegerField()
    base_learning_rate = models.FloatField()
    batch_size = models.PositiveIntegerField()
    epoch = models.PositiveIntegerField()
    train_percent = models.FloatField(default=1)
    create_time = models.DateTimeField(auto_now_add=True)
    update_time = models.DateTimeField(auto_now=True)


class MLModel(models.Model):
    class Meta:
        app_label = 'ml_app'

    def __str__(self):
        return "Model trained at: {0}".format(self.create_time.strftime("%Y-%m-%d, %H:%M:%S"))

    token_dict_path = models.CharField(max_length=255)
    model_path = models.CharField(max_length=255)
    config = models.ForeignKey(ModelConfig, on_delete=models.CASCADE)
    accurate = models.FloatField()
    precision = models.FloatField()
    recall = models.FloatField()
    f1 = models.FloatField()
    create_time = models.DateTimeField(auto_now_add=True)


@receiver(post_delete, sender=TaintFlow)
def delete_slice_files(sender, instance: TaintFlow, **kwargs):
    file = getattr(instance, 'slice_file')
    if os.path.isfile(file):
        os.remove(file)


@receiver(post_delete, sender=Label)
def delete_label_files(sender, instance: Label, **kwargs):
    file = instance.taint_flow.slice_file.replace("slice", "label")
    if os.path.isfile(file):
        os.remove(file)

