const multer = require('multer');
const path = require('path');

// configure storage location and filename
const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        cb(null, path.join(__dirname, '../../public/images')); // set save directory
    },
    filename: function (req, file, cb) {
        const ext = path.extname(file.originalname);
        const baseName = path.basename(file.originalname, ext);
        const uniqueName = `${baseName}${ext}`;
        cb(null, uniqueName);
    }
});

const upload = multer({ storage }); // create multer instance

module.exports = upload.single('image');