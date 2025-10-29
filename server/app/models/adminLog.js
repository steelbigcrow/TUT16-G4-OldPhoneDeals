const mongoose = require('mongoose');
const Schema = mongoose.Schema;

const AdminLogSchema = new Schema({
  adminUserId: {
    type: Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },
  action: {
    type: String,
    required: true,
    enum: [
      'CREATE_USER',
      'UPDATE_USER',
      'DELETE_USER',
      'DISABLE_USER',
      'ENABLE_USER',
      'CREATE_PHONE',
      'UPDATE_PHONE',
      'DELETE_PHONE',
      'DISABLE_PHONE',
      'ENABLE_PHONE',
      'HIDE_REVIEW',
      'SHOW_REVIEW',
      'DELETE_REVIEW',
      'EXPORT_ORDERS'
    ]
  },
  targetType: {
    type: String,
    required: true,
    enum: ['User', 'Phone', 'Review', 'Order']
  },
  targetId: {
    type: Schema.Types.ObjectId,
    required: true
  },
  createdAt: {
    type: Date,
    default: Date.now
  }
});

module.exports = mongoose.model('AdminLog', AdminLogSchema); 