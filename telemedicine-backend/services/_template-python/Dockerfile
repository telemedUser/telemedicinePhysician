FROM python:3.12-slim
WORKDIR /app

RUN pip install --no-cache-dir --upgrade pip setuptools wheel

COPY shared/python-common /tmp/python-common
RUN pip install --no-cache-dir /tmp/python-common && rm -rf /tmp/python-common

COPY services/_template-python/requirements.docker.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

COPY services/_template-python/src ./src
ENV PYTHONPATH=/app/src

EXPOSE 8082
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8082"]
