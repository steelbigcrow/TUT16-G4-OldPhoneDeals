//User.JSto create USerSchema in the application

//Including the required packages and assigning it to Local Variables
const mongoose = require('mongoose');
const Schema = mongoose.Schema;
const bcrypt = require('bcryptjs');



//Creating UserSchema 
const UserSchema = new Schema({
  firstName: { type: String, required: true },
  lastName: { type: String, required: true },
  email: {
    type: String,
    unique: true,
    required: true,
    lowercase: true ,
    match: /^\w+([\.-]?\w+)*@\w+([\.-]?\w+)*(\.\w{2,3})+$/
  },
  password: {
    type: String,
    required: true,
    select: false
  },
  wishlist: {                                                // user wishlist
    type: [{ type: Schema.Types.ObjectId, ref: 'Phone' }],
    default: []
  }, 
  isAdmin: { type: Boolean, default: false },
  isDisabled: { type: Boolean, default: false },
  isBan: { type: Boolean, default: false },
  isVerified: { type: Boolean, default: false },      // email verified
  verifyToken: { type: String },                      // email verification token
  lastLogin: Date,
  createdAt: { type: Date, default: Date.now },
  updatedAt: { type: Date, default: Date.now },
});

//Function to handleEvent of password modification 

UserSchema.pre('save', async function(next) {
  if (!this.isModified('password')) return next();
  try {
    console.log('user: pre encryption running start, password is ' + this.password);
    this.password = await bcrypt.hash(this.password, 12);
    console.log('user: pre encryption running end, password is ' + this.password);
    next();
  } catch (err) {
    next(err);
  }
});


//Function to check if modified and saved passwords match 

UserSchema.methods.comparePassword = async function(candidatePassword) {
  console.log('backend user: candidatePassword:', candidatePassword);
  console.log('backend user: this.password:', this.password);

  try {
    return await bcrypt.compare(candidatePassword, this.password);
  } catch (err) {
    throw new Error('Password comparison failed');
  }
};





//Exporting the Review schema to reuse
module.exports = mongoose.models.User || mongoose.model('User', UserSchema);



