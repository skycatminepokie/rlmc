FROM python:3.12

WORKDIR /usr/src/app

COPY . .

# Install Python packages
RUN python3 -m pip install --upgrade pip
RUN python3 -m pip install -r python/requirements.txt

# Install Java
RUN apt update && apt upgrade -y
RUN apt install -y wget apt-transport-https gpg
RUN wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor | tee /etc/apt/trusted.gpg.d/adoptium.gpg > /dev/null
RUN echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | tee /etc/apt/sources.list.d/adoptium.list
RUN apt update && apt upgrade -y
RUN apt install -y temurin-21-jdk

# Run Minecraft server
RUN echo "eula=true" >> run/eula.txt
CMD ./gradlew runServer