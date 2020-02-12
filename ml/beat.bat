del celerybeat.pid
call conda activate torch
python -m celery -A django_server beat -l info --scheduler django_celery_beat.schedulers:DatabaseScheduler
