from setuptools import setup, find_packages
import pathlib

setup(
    name="rlmc",
    version="0.0.1",
    packages=find_packages(where="python"),
    python_requires=">=3.7, <4",
)
