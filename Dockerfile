FROM skycatminepokie/py4j

WORKDIR /usr/src/app

# Install Python packages
ADD ./python/requirements.txt /usr/src/app/python/requirements.txt
RUN python3 -m pip install -r python/requirements.txt
ENV PYTHONPATH="${PYTHONPATH}:/usr/src/app/python"

# Prep the Minecraft server
RUN ["echo", "eula=true", ">>", "run/eula"]
EXPOSE 25565

COPY . .
RUN ["chmod", "+x", "./start.sh"]
RUN ["chmod", "+x", "./gradlew"]

CMD ["./start.sh"]