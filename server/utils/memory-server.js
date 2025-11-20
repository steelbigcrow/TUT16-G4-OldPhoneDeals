const { MongoMemoryServer } = require('mongodb-memory-server');

let mongoMemoryServer = null;

async function startMemoryServer() {
  if (mongoMemoryServer) {
    return mongoMemoryServer.getUri();
  }

  mongoMemoryServer = await MongoMemoryServer.create();
  return mongoMemoryServer.getUri();
}

async function stopMemoryServer() {
  if (!mongoMemoryServer) {
    return;
  }

  await mongoMemoryServer.stop();
  mongoMemoryServer = null;
}

module.exports = {
  startMemoryServer,
  stopMemoryServer
};
