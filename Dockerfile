# Adapted from CraftGround (https://github.com/yhs0602/CraftGround/blob/6e6644e0d137808e7e13520aa3baac4d603cf499/Dockerfile)
FROM ubuntu:22.04

WORKDIR /usr/src/app

# Install java and python
RUN apt-get update && apt-get install -y openjdk-21-jdk python3-pip

# Prep python
RUN python3 --version
RUN pip3 install --upgrade pip
ADD ./python/requirements.txt /usr/src/app/python/requirements.txt
RUN pip3 install -r ./python/requirements.txt

# Prep the Minecraft server
RUN ["echo", "eula=true", ">>", "run/eula"]
EXPOSE 25565
RUN ["chmod", "+x", "./gradlew"]

RUN ["chmod", "+x", "./start.sh"]

COPY . .

CMD ["./start.sh"]