#include <iostream>
#include <fstream>
#include <vector>
#include <cryptopp/files.h>
#include <cryptopp/aes.h>
#include <cryptopp/filters.h>

using namespace std;
using namespace CryptoPP;

void encryptFile(const string& filePath, const string& password) {
    // Generate a salt using a secure random number generator.
    byte salt[AES::DEFAULT_KEYLENGTH];
    SecByteBlock key(AES::DEFAULT_KEYLENGTH);
    PKCS5_PBKDF2_HMAC<SHA256> pbkdf2;
    pbkdf2.DeriveKey(key, key.size(), salt, (byte*)password.data(), password.size(), 100000);

    // Create a cipher using the derived key.
    CBC_Mode<AES>::Encryption cipher;
    cipher.SetKeyWithIV(key, key.size(), salt);

    // Read the file contents.
    ifstream ifs(filePath, ios::binary);
    if (!ifs.is_open()) {
        throw runtime_error("Could not open file: " + filePath);
    }
    string plaintext((istreambuf_iterator<char>(ifs)), istreambuf_iterator<char>());
    ifs.close();

    // Encrypt the file contents.
    string ciphertext;
    StreamTransformationFilter filter(cipher, new StringSink(ciphertext));
    filter.Put(reinterpret_cast<const unsigned char*>(plaintext.data()), plaintext.size());
    filter.MessageEnd();

    // Write the encrypted data to a new file.
    ofstream ofs(filePath + ".enc", ios::binary);
    if (!ofs.is_open()) {
        throw runtime_error("Could not open file: " + filePath + ".enc");
    }
    ofs.write(reinterpret_cast<const char*>(ciphertext.data()), ciphertext.size());
    ofs.close();

    // Delete the original file.
    remove(filePath.c_str());
}

void decryptFile(const string& filePath, const string& password) {
    // Read the encrypted file contents.
    ifstream ifs(filePath, ios::binary);
    if (!ifs.is_open()) {
        throw runtime_error("Could not open file: " + filePath);
    }
    string ciphertext((istreambuf_iterator<char>(ifs)), istreambuf_iterator<char>());
    ifs.close();

    // Generate a salt using the same method as when encrypting.
    byte salt[AES::DEFAULT_KEYLENGTH];
    memcpy(salt, ciphertext.data(), AES::DEFAULT_KEYLENGTH);

    // Derive a key from the password and salt using PBKDF2.
    SecByteBlock key(AES::DEFAULT_KEYLENGTH);
    PKCS5_PBKDF2_HMAC<SHA256> pbkdf2;
    pbkdf2.DeriveKey(key, key.size(), salt, (byte*)password.data(), password.size(), 100000);

    // Create a cipher using the derived key.
    CBC_Mode<AES>::Decryption cipher;
    cipher.SetKeyWithIV(key, key.size(), salt);

    // Decrypt the file contents.
    string plaintext;
    StreamTransformationFilter filter(cipher, new StringSink(plaintext));
    filter.Put(reinterpret_cast<const unsigned char*>(ciphertext.data()) + AES::DEFAULT_KEYLENGTH, ciphertext.size() - AES::DEFAULT_KEYLENGTH);
    filter.MessageEnd();

    // Write the decrypted data to a new file.
    ofstream ofs(filePath.substr(0, filePath.size() - 4), ios::binary);
    if (!ofs.is_open()) {
        throw runtime_error("Could not open file: " + filePath.substr(0, filePath.size() - 4));
    }
    ofs.write(plaintext.data(), plaintext.size());
    ofs.close();

    // Delete the encrypted file.
    remove(filePath.c_str());
}

int main() {
    // Get the file paths and password from the user.
    cout << "Enter the paths to the files to encrypt, separated by commas: ";
    string filePaths;
    getline(cin, filePaths);

    cout << "Enter a password: ";
    string password;
    getline(cin, password);

    // Encrypt the files.
    vector<string> paths;
    split(filePaths, paths, ',');
    for (const string& path : paths) {
        encryptFile(path, password);
    }

    // Decrypt the files.
    for (const string& path : paths) {
        decryptFile(path + ".enc", password);
    }

    return 0;
}
