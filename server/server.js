require('dotenv').config();
const path = require('path');
const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const { initDatabase } = require('./utils/init-db');

const app = express();
const port = 3000;

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

// database connection and initialization
// configure mongodb://mongoadmin:secret@some-mongo.orb.local:27017/ in .env file
mongoose.connect(process.env.MONGODB_URI)
    .then(async () => {
        console.log('Connected to MongoDB');
        
        // initialize database in development environment
        if (process.env.NODE_ENV === 'development') {
            try {
                await initDatabase();
            } catch (error) {
                console.error('Failed to initialize database:', error);
            }
        }
        
        // start server
        app.listen(port, () => {
            console.log(`后端：Server running at http://localhost:${port}`);
        });
    })
    .catch(err => {
        console.error('MongoDB connection error:', err);
        process.exit(1);
    });

