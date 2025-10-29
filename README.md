# OldPhoneDeals E-Commerce Platform

OldPhoneDeals is a full-stack e-commerce platform that allows users to buy and sell second-hand phones. It includes user authentication with email verification, password reset functionality, administrator control panels, and product listing features built with the MEAN stack (MongoDB, Express, Angular, Node.js).



## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.



###  Prerequisites

Before you begin, ensure you have the following installed:

- **Node.js** (v18+ recommended)
- **MongoDB** (Local instance running on port 27017)
- **Angular CLI** (`npm install -g @angular/cli`)
- **SendGrid account** (for sending email verification/reset)



### Installation Steps

1. **Clone repository**

    ```bash
    git clone <repository-url>
    cd TUT16-G4-OldPhoneDeals
    ```

2. **Install server backend dependencies**

    ```bash
    cd server 
    npm install
    ```
2. **Install angular frontend dependencies**

    ```bash
    cd AngularOldPhoneDeals 
    npm install
    ```
3. **Create Local Environment Config File**

   - creating .env file  
   - Your .env file should be placed in the server/ directory (at the same level as server.js)
   - The file content should be:
   
   ```bash
    MONGODB_URI=mongodb://localhost:27017/your-dataset-name
    NODE_ENV=development
    JWT_SECRET=TUT16G4
    SENDGRID_API_KEY=YOUR_SENDGRID_API_KEY_HERE
    FROM_EMAIL=hche0427@uni.sydney.edu.au
    FRONTEND_URL=http://localhost:4200
   ```
   - Make sure to replace `MONGODB_URI=mongodb://localhost:27017/your-dataset-name`  with you own database name.
   
   - ==`NODE_ENV=development` will auto-initialize the database on server startup. After first run then switch to `NODE_ENV=production` otherwise it will auto-initialize the database everytime.==

## Running the Full-Stack Application

Finally, let’s run the Node.js server. Ensure you are in the server directory and then run:

```bash
node server.js
```

Then, let’s run the Node.js client. Ensure you are in the client directory and then run:
```bash
npm start
```

Open your web browser and navigate to http://localhost:4200. You should see your Angular application served by the Node.js server.



## Notes on Registration & Email

- A real email is required to register because email verification is enforced.
- After registering, wait for the verification email and click the link before logging in.
- Ensure your `.env` file and SendGrid credentials are correctly configured.
- If emails are not received, check your spam folder or SendGrid account status.



##  Built With

- **Node.js** – Backend runtime
- **Express.js** – Backend framework
- **MongoDB** – NoSQL database
- **Angular** – Frontend framework
- **SendGrid** – Email service
- **JWT** – User authentication



## Authors

- Group 4 – COMP5347 / COMP4347
   See also the list of [contributors](#) who participated in this project.
