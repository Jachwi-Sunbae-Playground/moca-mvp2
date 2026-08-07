import React from 'react';
import logo from './assets/logo.svg';
import './style.css';

const App = () => {
  return (
    <div className="container">
      <img src={logo} alt="Logo" className="logo"></img>
      <h1>자취선배</h1>
      <div className="init">HELLO WORLD</div>
    </div>
  );
};

export default App;
