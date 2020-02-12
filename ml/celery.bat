call  conda activate torch
python -m celery worker -A django_server -l info -P eventlet

