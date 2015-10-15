![Logo](src/main/resources/com/edduarte/protbox/splash.png)

## Paper
Secure and trustworthy file sharing over cloud storage using eID tokens [(link)](http://arxiv.org/abs/1501.03139)  
Lecture Notes in Informatics (LNI), Proceedings - Series of the Gesellschaft fur Informatik (GI), Volume P-237, 2014, Pages 73-84  
Open Identity Summit 2014; Stuttgart; Germany; 4 November 2014 through 6 November 2014; Code 109544

## Introduction
Protbox is a multi-platform application that aims to introduce confidentiality and access control to data stored in existing cloud storage services.

Confidentiality is provided in a transparent way, similarly to the synchronization with the cloud repositories. Confidential data can be made accessible to others sharing the same cloud folder by means of signed requests exchanged through the same cloud storage; no extra central services are used.

The secure sharing includes three different protection attributes:
- confidentiality, to prevent non-authorized readings;
- integrity control, to detect malicious tampering;
- protection against involuntary file removals, either by malicious or legitimate persons.

The strong authentication of people, which is used to enforce the access control to the shared data, relies on the exploitation of nowadays existing electronic, personal identity tokens (eIDs for short).

Protbox randomly generates and uses a key per folder to protect all its contents, including files and sub-directories. Files are encrypted with AES and their integrity is ensured with HMAC-SHA512. Encrypted file names, which contain bytes that are not acceptable for naming files in existing file systems, are coded in a modified Base64 alphabet (formed by letters, decimal digits, underscore, hyphen and dot), which should work in most file systems.

This is a Java prototype that should be able to run on any operating system with a suitable Java Virtual Machine (JVM), and is capable of recognizing any file system. It features a background folder synchronization engine and a graphical user interface for dealing with key distribution requests. This prototype was successfully experimented in both Linux and Windows with three major cloud storage providers: Dropbox, OneDrive and Google Drive.

## How to run
To run the application, use the following commands on the source folder:

```
mvn package
java -jar target/protbox-2.0-app.jar
```

Alternatively, download the [v2.0 release](https://github.com/com.edduarte/protbox/releases/tag/v2.0), unpack the compressed file and use the following command:

```
java -jar protbox-2.0-app.jar
```

The application will need to know where to find the PKCS#11 provider that is capable of reading the eID token to be used. In order to include support for PKCS#11 providers in the application, a configuration file must be added to the 'providers' folder (in the same folder as the jar file) with the suffix '.config' and the following contents:

```
name=[Name of the provider]
library=[Local path of the provider]
alias=[Alias of the authentication certificate in the eID token]
```

The 'example' folder contains the required files to read and use the Portuguese Citizen Card for authentication in Protbox.
