![Logo](imgs/protbox-logo-v2-header-small.png)
=======

**Multi-platform application** that aims to introduce confidentiality and access control to data stored in existing cloud storage services.

Confidentiality is provided in a transparent way, similarly to the synchronization with the cloud repositories. Confidential data can be made accessible to others sharing the same cloud folder by means of signed requests exchanged through the same cloud storage; no extra central services are used.

The secure sharing includes three different protection attributes:
- confidentiality, to prevent non-authorized readings;
- integrity control, to detect malicious tampering;
- protection against involuntary file removals, either by malicious or legitimate persons.

The strong authentication of people, which is used to enforce the access control to the shared data, relies on the exploitation of nowadays existing electronic, personal identity tokens (eIDs for short).

Protbox randomly generates and uses a key per folder to protect all its contents, including files and sub-directories. Files are encrypted with AES or Triple-DES (128 bit keys) and their integrity is ensured with HMAC-SHA1. Encrypted file names, which contain bytes that are not acceptable for naming files in existing file systems, are coded in a modified Base64 alphabet (formed by letters, decimal digits, underscore, hyphen and dot), which should work in most file systems.


This is a Java prototype that should be able to run on any operating system with a suitable Java Virtual Machine (JVM), and is capable of recognizing any file system. It features a background folder synchronization engine and a graphical user interface for dealing with key distribution requests. This prototype was successfully experimented in both Linux and Windows with three major cloud storage providers: Dropbox, OneDrive and Google Drive.
