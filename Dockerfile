FROM skycatminepokie/py4j

WORKDIR /usr/src/app

# Install Python packages
ADD ./python/requirements.txt /usr/src/app/python/requirements.txt
RUN python3 -m pip install -r python/requirements.txt

COPY . .

# Run Minecraft server
RUN ["echo", "eula=true", ">>", "run/eula"]
EXPOSE 25565
CMD ["python3", "python/skycatdev/rlmc/entrypoint.py", "&", "./gradlew runServer"]