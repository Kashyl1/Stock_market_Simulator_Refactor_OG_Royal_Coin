CREATE ROLE trading_simulator WITH LOGIN PASSWORD 'trading_simulator';
CREATE DATABASE trading_simulator OWNER trading_simulator;
GRANT ALL PRIVILEGES ON DATABASE trading_simulator TO trading_simulator;
