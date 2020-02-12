@echo off
echo "Launch MySQL and Redis first"
call activate torch
start /b python -m celery -A django_server beat -l info --scheduler django_celery_beat.schedulers:DatabaseScheduler
start /b python -m celery worker -A django_server -l info -P eventlet
python manage.py runserver
REM  call conda deactivate
