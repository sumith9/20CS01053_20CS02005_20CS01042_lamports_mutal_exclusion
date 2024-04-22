# Lamport Mutual Exclusion for Distributed File Access

This project implements the Lamport mutual exclusion algorithm. The system allows multiple clients to perform read and write operations on files hosted by a central server while ensuring mutual exclusion and synchronization among them.

## Components

### Server
- The server component acts as the central coordinator for file access operations.
- It implements the Lamport mutual exclusion algorithm to ensure that only one client accesses a file at a time.
- The server listens for incoming client connections and coordinates file access requests.

### Client
- Clients connect to the server to request access to files and perform read and write operations.
- Each client implements the Lamport mutual exclusion algorithm to ensure synchronized access to files.
- Clients communicate with the server and other clients to coordinate file access operations.

## Usage

## Lamport Mutual Exclusion Algorithm
- The Lamport mutual exclusion algorithm ensures that only one client can access a file at a time.
- Clients use Lamport clocks to timestamp their requests and determine the order of access.
- The algorithm guarantees mutual exclusion, progress, and fairness in file access operations.

## Implementation Details
- The system utilizes Java sockets for communication between the server and clients.
- Multi-threading is employed to handle concurrent client connections and file access operations.
- Clients implement the Lamport mutual exclusion algorithm to coordinate access to files.
- Synchronization mechanisms ensure proper coordination and mutual exclusion in distributed file access.

## Getting Started
Step 1: Copy Server.java and Client.java the machines <br />

Step 2: Compile and run Server.java on server machine using below commands <br />

   ```
   javac Server.java 	
   ```
   ```
   java Server
   ```
   
Step 3: Compile and run Client.java on all client machines using below commands <br />
   ```
   javac Client.java 	
   ```
   ```
   java Client <Node_Number>	
   ```
Node_Numbers: 1 for Client1, 2 for Client2, 3 for Client3 <br />
   
Step 4: Each of the 3 clients performs 5 random Read/Write operations and prints the output 

## Submitted by
Pooner Sumith 20CS01053 <br />
Mothukuri Likhitha 20CS02005 <br />
Katkam Raja Vishwa Teja 20CS01042
