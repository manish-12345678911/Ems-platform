FROM python:3.11-slim
WORKDIR /app
COPY . /app
ENV PORT=8088
EXPOSE 8088
CMD ["python", "server.py"]
