const fs = require('fs').promises;
const path = require('path');
const Phone = require('../app/models/phone');
const User = require('../app/models/user');
const mongoose = require('mongoose');
const Cart = require('../app/models/cart');
const Order = require('../app/models/order');
const AdminLog = require('../app/models/adminLog');

async function initDatabase() {
  try {
    console.log('Starting database initialization...');
    
    // clear existing collections
    await Promise.all([
      Phone.deleteMany({}),
      User.deleteMany({}),
      Cart.deleteMany({}),
      Order.deleteMany({}),
      AdminLog.deleteMany({})
    ]);
    console.log('Existing collections cleared');
    
    // read initial data
    const phonesRaw = JSON.parse(
      await fs.readFile(path.join(__dirname, '../data/phonelisting.json'), 'utf8')
    );
    const usersRaw = JSON.parse(
      await fs.readFile(path.join(__dirname, '../data/userlist.json'), 'utf8')
    );
    console.log('Initial data loaded from JSON files');
    
    // data mapping: fix image path, reviews isHidden, user field names and ObjectId conversion
    const phonesData = phonesRaw.map(phone => ({
      title: phone.title,
      brand: phone.brand,
      image: `/static/images/${phone.brand}.jpeg`,
      stock: phone.stock,
      seller: new mongoose.Types.ObjectId(String(phone.seller)),
      price: phone.price,
      reviews: (phone.reviews || []).map(r => ({
        reviewerId: new mongoose.Types.ObjectId(String(r.reviewer)),
        rating: r.rating,
        comment: r.comment,
        isHidden: r.isHidden ?? ('hidden' in r ? true : false)
      }))
    }));
    
    // user mapping
    const usersData = usersRaw.map(u => ({
      _id: new mongoose.Types.ObjectId(String(u._id.$oid)),
      firstName: u.firstname,
      lastName: u.lastname,
      email: u.email,
      password: u.password
    }));

    // insert data
    await Phone.insertMany(phonesData);
    await User.insertMany(usersData);
    
    console.log('Database initialized successfully');
  } catch (error) {
    console.error('Error initializing database:', error);
    throw error;
  }
}

module.exports = { initDatabase }; 