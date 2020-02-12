#!/usr/bin/env python3
"""

:Author: Anemone Xu
:Email: anemone95@qq.com
:copyright: (c) 2020 by Anemone Xu.
:license: Apache 2.0, see LICENSE for more details.
"""
from __future__ import absolute_import, unicode_literals
import os
from celery import Celery

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'django_server.settings')

app = Celery('MSCA')

app.config_from_object('django.conf:settings', namespace='CELERY')

app.autodiscover_tasks()
