require('dotenv').config();
const path = require('path');
const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const { initDatabase } = require('./utils/init-db');
const { startMemoryServer, stopMemoryServer } = require('./utils/memory-server');

const app = express();
const port = process.env.PORT || 3000;
const isTestEnv = process.env.NODE_ENV === 'test';
const isDevEnv = process.env.NODE_ENV === 'development';
let server;

// middlewares
app.use(cors({
    origin: 'http://localhost:4200',
    credentials: true
}));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// auth router
const authRouter = require('./app/routes/user.routes');
app.use('/api', authRouter);

// phone router
const phoneRouter = require('./app/routes/phone.routes');
app.use('/api', phoneRouter);

// cart router
const cartRouter = require('./app/routes/cart.routes');
app.use('/api', cartRouter);

// order router
const orderRouter = require('./app/routes/order.routes');
app.use('/api', orderRouter);

// wishlist router
const wishlistRouter = require('./app/routes/wishlist.routes');
app.use('/api', wishlistRouter);

// admin auth router
const adminAuthRouter = require('./app/routes/admin.routes');
app.use('/api/admin', adminAuthRouter);

// admin user router
const adminUserRouter = require('./app/routes/adminUser.routes');
app.use('/api/admin', adminUserRouter);

// admin phone router
const adminPhoneRouter = require('./app/routes/adminPhone.routes');
app.use('/api/admin', adminPhoneRouter);

// admin review router
const adminReviewRouter = require('./app/routes/adminReview.routes');
app.use('/api/admin', adminReviewRouter);

// admin order router
const adminOrderRouter = require('./app/routes/adminOrder.routes');
app.use('/api/admin', adminOrderRouter);

// 
const profileRouter = require('./app/routes/profile.routes');
app.use('/api', profileRouter);

// backend static files
// URL mapping: /public/images/brand.jpeg (local) -> /static/images/brand.jpeg (virtual)
app.use('/static', express.static(path.join(__dirname, 'public')));

// frontend static files
app.use(express.static(path.join(__dirname, '../AngularOldPhoneDeals/dist/angular-old-phone-deals')));

// other test interfaces
app.get('/api/test', (req, res) => {
    res.json({ message: 'API is working' });
});
app.get('/', (req, res) => {
    res.send('Welcome to Old Phone Deals!');
});


// error handling middleware
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).send('Something broke!');
});

async function connectToDatabase() {
    let mongoUri = process.env.MONGODB_URI;

    if (isTestEnv) {
        mongoUri = await startMemoryServer();
        console.log(`MongoMemoryServer started for tests at ${mongoUri}`);
    }

    if (!mongoUri) {
        throw new Error('MONGODB_URI is not defined.');
    }

    await mongoose.connect(mongoUri);
    console.log('Connected to MongoDB');

    // initialize database in development and test environments
    if (isDevEnv || isTestEnv) {
        try {
            await initDatabase();
        } catch (error) {
            console.error('Failed to initialize database:', error);
        }
    }
}

async function startServer() {
    await connectToDatabase();

    server = app.listen(port, () => {
        console.log(`??:Server running at http://localhost:${port}`);
    });
}

async function shutdown() {
    if (server) {
        await new Promise(resolve => server.close(resolve));
    }

    await mongoose.connection.close();

    if (isTestEnv) {
        await stopMemoryServer();
    }
}

function exitProcess(code) {
    shutdown()
        .then(() => process.exit(code))
        .catch(err => {
            console.error('Error during shutdown:', err);
            process.exit(code);
        });
}

process.on('SIGINT', () => exitProcess(0));
process.on('SIGTERM', () => exitProcess(0));
process.on('unhandledRejection', reason => {
    console.error('Unhandled promise rejection:', reason);
    exitProcess(1);
});

startServer().catch(err => {
    console.error('Failed to start server:', err);
    exitProcess(1);
});
