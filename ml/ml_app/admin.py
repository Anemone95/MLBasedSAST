from django.contrib import admin
from .models import *

# Register your models here.
admin.site.site_header = 'MSCA后台管理系统'
admin.site.site_title = 'MSCA Admin'


@admin.register(ClientToken)
class TokenAdmin(admin.ModelAdmin):
    list_display = ('token', 'description', 'create_time')
    list_per_page = 20


admin.site.register(Project)


@admin.register(MethodDescription)
class MethodDescriptionAdmin(admin.ModelAdmin):
    list_display = ('clazz', 'method', 'sig', 'project')
    list_per_page = 20


@admin.register(Location)
class LocationAdmin(admin.ModelAdmin):
    list_display = ('source_file', 'start_line', 'end_line', 'project')
    list_per_page = 20


@admin.register(TaintFlow)
class TaintFlowAdmin(admin.ModelAdmin):
    list_display = ('hash', 'project')
    list_per_page = 20


@admin.register(Label)
class LabelAdmin(admin.ModelAdmin):
    list_display = ('taint_flow', 'is_safe')
    list_per_page = 20


@admin.register(ModelConfig)
class ModelConfigAdmin(admin.ModelAdmin):
    list_display = ('name',
                    'slice_dir',
                    'label_dir',
                    'embedding_dim',
                    'hidden_dim',
                    'word_freq_gt',
                    'early_stop_patience',
                    'base_learning_rate',
                    'batch_size',
                    'epoch',
                    'train_percent'
                    )
    list_per_page = 20


@admin.register(MLModel)
class MLModelAdmin(admin.ModelAdmin):
    list_display = (
        'token_dict_path',
        'model_path',
        'config',
        'accurate',
        'precision',
        'recall',
        'f1')
    list_per_page = 50

