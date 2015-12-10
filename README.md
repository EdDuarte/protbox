![Logo](src/main/resources/com/edduarte/protbox/splash.png)

Protbox is a multi-platform application that aims to introduce confidentiality and access control to data stored in existing cloud storage services. It features a background folder synchronization engine and a graphical user interface for dealing with key distribution requests. It was successfully tested in both Linux and Windows with three major cloud storage providers: Dropbox, OneDrive and Google Drive.

# Publication
** Secure and trustworthy file sharing over cloud storage using eID tokens **  
Lecture Notes in Informatics (LNI), Proceedings - Series of the Gesellschaft fur Informatik (GI), Volume P-237, 2014, Pages 73-84  
Open Identity Summit 2014; Stuttgart; Germany; 4 November 2014 through 6 November 2014; Code 109544  
[Publication webpage](http://edduarte.com/talks/openidentity2014/)

# Description
Confidentiality is provided in a transparent way, similarly to the synchronization with the cloud repositories. Confidential data can be made accessible to others sharing the same cloud folder by means of signed requests exchanged through the same cloud storage; no extra central services are used.

The secure sharing includes three different protection attributes:
- confidentiality, to prevent non-authorized readings;
- integrity control, to detect malicious tampering;
- protection against involuntary file removals, either by malicious or legitimate persons.

The strong authentication of people, which is used to enforce the access control to the shared data, relies on the exploitation of nowadays existing electronic, personal identity tokens (eIDs for short).

Protbox randomly generates and uses a key per folder to protect all its contents, including files and sub-directories. Files are encrypted with AES and their integrity is ensured with HMAC-SHA512. Encrypted file names, which contain bytes that are not acceptable for naming files in existing file systems, are coded in a modified Base64 alphabet (formed by letters, decimal digits, underscore, hyphen and dot), which should work in most file systems.

# Getting Started
To run the application, use the following commands on the source folder:

```
mvn package
java -jar target/protbox-3.0.2-app.jar
```

Alternatively, download the [3.0.2 release](https://github.com/edduarte/protbox/releases/tag/3.0.2), unpack the compressed file and use the following command:

```
java -jar protbox-3.0.2-app.jar
```

The application will need to know where to find the PKCS#11 provider that is capable of reading the eID token to be used. In order to include support for PKCS#11 providers in the application, a configuration file must be added to the 'providers' folder (in the same folder as the jar file) with the suffix '.config' and the following contents:

```
name=[Name of the provider]
library=[Local path of the provider]
alias=[Alias of the authentication certificate in the eID token]
```

The 'example' folder contains the required files to read and use the Portuguese Citizen Card for authentication in Protbox.

# License

    Copyright 2014 University of Aveiro

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
