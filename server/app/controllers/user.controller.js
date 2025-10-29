const jwt = require('jsonwebtoken');
const User = require('../models/user');
const sgMail = require('@sendgrid/mail');

const crypto = require('crypto');

sgMail.setApiKey(process.env.SENDGRID_API_KEY);
// Ab!A123123


// Login controller
exports.login = async (req, res) => {
    console.log('后端 user.login：收到登录请求，body:', req.body);

    // ====== test user code start (for testing only) ======
    const testEmail = 'chenhao@fake.com';
    const testPassword = 'Ab!A123123';
    const exists = await User.findOne({ email: testEmail });
    if (!exists) {
        await User.create({
            firstName: 'Hao',
            lastName: 'Chen',
            email: testEmail,
            password: testPassword,
            isAdmin: false,
            isDisabled: false,
            isBan: false,
            isVerified: true,
            verifyToken: null,
            lastLogin: null,
            createdAt: new Date(),
            updatedAt: null
        });
    }
    // ====== test user code completed ======

    try {
        const { email, password } = req.body;

        // 1. Check if the user exists
        const user = await User.findOne({ email }).select('+password');
        // console.log('后端 user.login：查到的用户:', user);
        if (!user) {
            return res.json({
                success: false,
                message: 'Invalid email or password'
            });
        }
        // 2. 验证密码
        const isMatch = await user.comparePassword(password);
        // console.log('后端 user.login：密码情况:', isMatch);
        if (!isMatch) {
            return res.json({
                success: false,
                message: 'Invalid email or password'
            });
        }

        // 检查邮箱是否已验证
        // console.log('后端 user.login：检查是否邮箱激活:', user.isVerified);
        if (!user.isVerified) {
            return res.json({
                success: false,
                message: 'Email not verified'
            });
        }

        // 3. 检查账户状态
        if (user.isDisabled) {
            return res.json({
                success: false,
                message: 'Account has been disabled'
            });
        }
        // console.log('后端 user.login：用户是否被封禁:', user.isDisabled);

        // 4. 生成JWT令牌（支持 JWT_SECRET 或 SECRET 环境变量）
        const secret = process.env.JWT_SECRET || process.env.SECRET;
        const token = jwt.sign(
            { id: user._id, email: user.email },
            secret,
            { expiresIn: '7d' }
        );
        // console.log('后端 user.login：显示token:', token);

        // 5. 更新最后登录时间
        user.lastLogin = new Date();
        await user.save();
        // console.log('后端：登录成功，更新登入时间，用户ID:', user._id);

        res.json({
            success: true,
            token,
            user: {
                id: user._id,
                firstName: user.firstName,
                lastName: user.lastName,
                email: user.email
            }
        });

    } catch (err) {
        // console.error('后端 user.login：登录异常', err);
        return res.json({
            success: false,
            message: err.message
        });
    }
};

// register controller
exports.register = async (req, res) => {
    try {
        const { firstName,lastName, email, password } = req.body;
        // console.error('后端 user.register：请求体', req.body);
        // 1. 校验参数
        if (!firstName || !lastName || !email || !password) {
            // return res.json({ success: false, message: 'All fields are required.' });
            return res.json({
                success: false,
                message: 'All fields are required.'
            });
        }



        // 2. 检查邮箱是否已注册
        const existingUser = await User.findOne({ email });
        if (existingUser) {
            // console.error('后端 user.register：检查邮箱是否已注册: yes');
            return res.json({
                success: false,
                message: 'Account with that email is already exist'
            });
        }



        // 3. 检查用户名是否已存在（可选）
        const existingUsername = await User.findOne({ firstName, lastName });
        if (existingUsername) {
            // console.error('后端 user.register：检查用户名是否已存在: yes');
            // return res.json({ success: false, message: 'Username already exists.' });
            return res.json({
                success: false,
                message: 'Username already exists.'
            });
        }




        // 4. 生成唯一token
        const verifyToken = crypto.randomBytes(32).toString('hex');
        // 5. 创建新用户
        const user = new User({
            firstName, // 或者分割 username 得到 firstName/lastName
            lastName,
            email,
            password, // 明文，pre('save') 会自动加密
            isAdmin: false,
            isDisabled: false,
            isBan: false,
            isVerified: false,
            verifyToken,
            lastLogin: null,
            createdAt: new Date(),
            updatedAt: new Date()
        });

        await user.save();



        // 5. 发送激活邮件
        const verifyUrl = `${process.env.FRONTEND_URL}/verify-email?token=${verifyToken}&email=${encodeURIComponent(email)}`;
        const msg = {
            to: email,
            from: process.env.FROM_EMAIL,
            subject: 'Activate your OldPhoneDeals account',
            html: `
      <p>Hi ${firstName},</p>
      <p>Welcome! Please click the link below to activate your account:</p>
      <a href="${verifyUrl}">${verifyUrl}</a>
      <p>Thanks,<br/>The OldPhoneDeals Team</p>
    `
        };
        await sgMail.send(msg);

        // 返回注册成功
        return res.json({
            success: true,
            message: 'Registration successful! Please check your email to verify your account.' });

    } catch (err) {
        console.error('Registration error:', err);
        // res.json({ success: false, message: 'Server error: ' + err.message });
        return res.json({
            success: false,
            message: 'Server error: ' + err.message });
    }
};

// 检查user 邮箱是否激活
exports.verifyEmail = async (req, res) => {
    console.error('后端 user.verifyEmail：进入邮箱验证');
    const { token, email } = req.query;
    const user = await User.findOne({ email, verifyToken: token });
    if (!user) {
        return res.json({ 
            success: false, 
            message: 'Invalid or expired activation link.' });
    }
    user.isVerified = true;
    user.verifyToken = undefined;
    await user.save();
    // console.error('后端 user.verifyEmail：完成邮箱验证');
    // 可重定向到前端登录页或显示激活成功提示
    res.json({ success: true, message: 'Account activated! You can now log in.' });
};

// 检查user 邮箱是否存在
exports.sendRestPasswordEmail = async (req, res) => {
    // console.log('后端 user.check Email Exist：进入邮箱验证');
    const { email } = req.body;

    if (!email) {
        return res.json({
            success: false,
            message: 'Email is required！' });
    }

    try {
        const user = await User.findOne({ email: email });

        if (user) {

            // 构建前端跳转链接（只带 email）
            const resetUrl = `${process.env.FRONTEND_URL}/reset-password?email=${encodeURIComponent(user.email)}`;
            // 构建邮件内容
            const msg = {
                to: user.email,
                from: process.env.FROM_EMAIL,
                subject: 'Reset your OldPhoneDeals password',
                html: `
                  <p>Hello ${user.firstName || 'User'},</p>
                  <p>You requested to reset your password. Please click the link below:</p>
                  <a href="${resetUrl}">${resetUrl}</a>
                  <p>If you didn't request a password reset, you can ignore this email.</p>
                  <p>Thanks,<br/>OldPhoneDeals Team</p>
                `
            };
            await sgMail.send(msg);

            return res.json({
                success: true,
                message: 'Reset password email sent successfully!'
            });

        } else {
            return res.json({
                success: false,
                message: 'User not found' });
        }
    } catch (error) {
        console.error('send email failed:', error);
        return res.json({ success: false, message: error });
    }
};


exports.checkVerified = async (req, res) => {
    const email = req.query.email;
    if (!email) return res.json({ isVerified: false });
    const user = await User.findOne({ email });
    if (!user) return res.json({ isVerified: false });
    res.json({ isVerified: user.isVerified });
};

// 获取用户信息
exports.getUserInfo = async (req, res) => {
    // console.error('后端 user.getUserInfo：正在进行验证');
    try {
        const token = req.headers['authorization']?.split(' ')[1]; // 从请求头中获取 token
        if (!token) {
            return res.json({ 
                success: false,
                message: 'No token provided.' });
        }

        // 验证 token
        jwt.verify(token, process.env.JWT_SECRET, async (err, decoded) => {
            if (err) {
                return res.json({ 
                    success: false, 
                    message: 'Invalid token.' });
            }

            // 从 token 解码中获取用户 ID
            const userId = decoded.id;

            // 查询用户信息
            const user = await User.findById(userId).select('-password');  // 不返回密码
            if (!user) {
                return res.json({ 
                    success: false, 
                    message: 'User not found.' });
            }

            // 返回用户信息
            res.json({
                success: true,
                user: {
                    id: user._id,
                    firstName: user.firstName,
                    lastName: user.lastName,
                    email: user.email,
                    isAdmin: user.isAdmin,
                    isDisabled: user.isDisabled,
                    isBan: user.isBan
                }
            });
            // console.error('后端 user.getUserInfo：返回用户信息',  res.json);
        });
    } catch (err) {
        // console.error('后端 user.getUserInfo：获取用户信息失败', err);
        res.json({
            success: false,
            message: 'Server error: ' + err.message });
    }
};

// 修改密码
exports.resetPassword = async (req, res) => {
    // Ab!A123125
    try {
        const { email, currentPassword, newPassword } = req.body;
        console.error('后端 user.resetPassword req.body', req.body)
        // 确保所有字段都有传入
        // email 和 newPassword 必须存在
        if (!email || !newPassword) {
            return res.json({ success: false, message: 'Email and newPassword are required' });
        }
        // 1. 查找用户
        const userOne = await User.findOne({ email: email }).select('+password');
        // console.error('后端 user.resetPassword userOne', userOne)
        if (!userOne) {
            // console.error('后端 user.resetPassword meizhaoda')
            return res.json({ success: false, message: 'User not found' });
        }

        // 如果传了 currentPassword，则验证
        if (currentPassword) {
            const isMatch = await userOne.comparePassword(currentPassword);
            if (!isMatch) {
                return res.json({ success: false, message: 'Current password is incorrect' });
            }
            if (currentPassword === newPassword) {
                return res.json({ success: false, message: 'The new password cannot be the same as the current password' });
            }
        }
        // 如果没传 currentPassword，跳过旧密码校验

        // 3. 可选：密码强度检查（8位，含大小写、数字、特殊字符）
        const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[\W_]).{8,}$/;
        if (!passwordRegex.test(newPassword)) {
            return res.json({ success: false, message: 'Password must be at least 8 characters and include uppercase,' +
                    ' lowercase, number, and special character.' });
        }

        // 4. 设置新密码并保存
        userOne.password = newPassword;
        await userOne.save();

        // 5. 发送邮件通知
        const msg = {
            to: userOne.email,
            from: process.env.FROM_EMAIL,
            subject: 'Your OldPhoneDeals account password changed successfully',
            html: `
        <p>Hi ${userOne.firstName},</p>
        <p>We wanted to let you know that your password has been successfully changed.</p>
        <p>If you did not make this change, please contact our support team immediately to secure your account.</p>
        <p>Thanks,<br/>The OldPhoneDeals Team</p>
      `
        };
        await sgMail.send(msg);

        res.json({ success: true, message: 'Password changed successfully' });

    } catch (err) {
        console.error('Change password error:', err);
        res.json({ success: false, message: 'Server error: ' + err.message });
    }
};


