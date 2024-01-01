import os
import base64
import hashlib
import binascii
import cryptography
from cryptography.fernet import Fernet
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC

def encrypt_files(file_paths, password):
    """
    Encrypts multiple files using the given password.

    Args:
        file_paths (list): A list of paths to the files to encrypt.
        password (str): The password to use for encryption.
    """

    for file_path in file_paths:
        encrypt_file(file_path, password)

def decrypt_files(file_paths, password):
    """
    Decrypts multiple files using the given password.

    Args:
        file_paths (list): A list of paths to the files to decrypt.
        password (str): The password to use for decryption.
    """

    for file_path in file_paths:
        decrypt_file(file_path, password)

if __name__ == "__main__":
    # Get the file paths and password from the user.
    file_paths = input("Enter the paths to the files to encrypt, separated by commas: ").split(",")
    password = input("Enter a password: ")

    # Encrypt the files.
    encrypt_files(file_paths, password)

    # Decrypt the files.
    decrypt_files(file_paths, password)
