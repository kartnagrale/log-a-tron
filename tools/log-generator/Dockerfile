FROM python:3.13.7-alpine
WORKDIR /app
COPY generator.py /app/generator.py
ENTRYPOINT ["python","/app/generator.py"]
CMD ["--seed","42","--scenario","all","--count","80","--rate","20"]