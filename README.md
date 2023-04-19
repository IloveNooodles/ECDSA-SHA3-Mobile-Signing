# ECDSA-SHA3-Mobile-Signing
Repository for Tugas Besar 3 Kriptografi Penerapan Elliptic Curve Cryptography ECDSA dan SHA-3 untuk Menandatangani Surel pada Perangkat Mobile


## Members
|   NIM    |              Name              |
| :------: | :----------------------------: |
| 13520029 | Muhammad Garebaldhie ER Rahman |
| 13520163 |     Frederik Imanuel Louis     |
| 13520166 |       Raden Rifqi Rahman       |

## Requirements
1. Python 3.9+
2. Java 11+
3. Kotlin
4. Gradle 7.4+
5. Android Studio

## How it works
We use `sign-then-encrypt` method for this. There will be client and server, client will send the data to the server to sign the message then encrypt it so

### General
![image](https://user-images.githubusercontent.com/63847012/233086633-7e195a7a-6475-4a5f-b9c6-dbd4b97ff878.png)

### Sign
![image](https://user-images.githubusercontent.com/63847012/233088585-86c9c321-5d28-4964-9596-2cfbea3d6002.png)

### Verify
![image](https://user-images.githubusercontent.com/63847012/233088648-844b9fe1-3339-46bd-b5d7-6015ef64a5f3.png)


## How to run
1. Clone repository by using `git clone https://github.com/IloveNooodles/ECDSA-SHA3-Mobile-Signing`
2. Open `src` folder and run `python app.py` to run the server
3. Open `k-9-main` folder WITH `android studio`
4. Make sure you're using `java11+`
5. Change `API_URL` to your server address in the program
6. If you want to use localhost make sure to add proxies by going to `file > settings > System settings > http proxy` and check manual proxy configuration, fill it with your ip address.
