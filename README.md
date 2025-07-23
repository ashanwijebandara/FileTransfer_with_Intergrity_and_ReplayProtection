# Document Sharing Application

## Overview
This project is a document sharing application that allows users to send and receive files between a client and a server. The application features a graphical user interface (GUI) for easy interaction and file selection.


## File Descriptions

### `src/Client.java`
This file contains the `Client` class which is responsible for sending files to the server. It provides a GUI for the user to select a file and send it. The file is read into a byte array and sent over a socket connection to the server.

### `src/Server.java`
This file contains the `Server` class which listens for incoming connections from clients. It receives files sent by clients and displays them in a GUI. The server handles file reception and updates the user interface accordingly.

### `src/MyFile.java`
This file defines the `MyFile` class which represents a file with properties such as `id`, `name`, `data`, and `fileExtension`. It includes getter and setter methods for these properties.

### `src/utils/FileUtils.java`
This file contains utility methods for file operations, such as reading and writing files. It can be used to simplify file handling in both the client and server classes.

## Setup Instructions
1. **Clone the Repository**: Clone this repository to your local machine.
2. **Navigate to the Project Directory**: Open a terminal and navigate to the `Document_Share` directory.
3. **Compile the Source Code**: Use the following command to compile the Java files:
   ```
   javac src/*.java src/utils/*.java
   ```
4. **Run the Server**: Start the server by executing:
   ```
   java -cp src Server
   ```
5. **Run the Client**: In a new terminal, start the client by executing:
   ```
   java -cp src Client
   ```

## Dependencies
- Java Development Kit (JDK) 8 or higher is required to compile and run the application.

## Usage
- Use the client interface to choose a file and send it to the server.
- The server will display the received files in its GUI.
- The server can also send files back to the client, allowing for bidirectional file sharing.

## Future Enhancements
- Implement additional features such as file type filtering, progress indicators for file transfers, and error handling for network issues.
- Consider adding support for multiple clients connecting to the server simultaneously.
