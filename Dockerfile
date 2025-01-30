FROM python:3.12

WORKDIR /usr/src/app

RUN git clone https://github.com/skycatminepokie/rlmc.git
RUN cd rlmc
RUN python3 -m pip install --upgrade pip
RUN python3 -m pip install -r requirements.txt && apt install openjdk-21