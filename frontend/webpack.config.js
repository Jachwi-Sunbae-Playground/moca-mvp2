const path = require('path');
const webpack = require('webpack');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const { BundleAnalyzerPlugin } = require('webpack-bundle-analyzer');

const shouldAnalyze = process.env.ANALYZE === 'true';
const isBrowserTestHarness = process.env.BROWSER_TEST_HARNESS === 'true';

module.exports = {
  entry: isBrowserTestHarness ? './src/test-browser/main.tsx' : './src/main.tsx',
  output: {
    publicPath: '/',
  },
  plugins: [
    new webpack.DefinePlugin({
      __API_BASE_URL__: JSON.stringify(process.env.API_BASE_URL ?? ''),
      __GOOGLE_CLIENT_ID__: JSON.stringify(process.env.GOOGLE_CLIENT_ID ?? ''),
      __GOOGLE_REDIRECT_URI__: JSON.stringify(process.env.GOOGLE_REDIRECT_URI ?? ''),
    }),
    new HtmlWebpackPlugin({
      template: './index.html',
      filename: 'index.html',
      inject: true,
    }),
    new BundleAnalyzerPlugin({
      analyzerMode: shouldAnalyze ? 'static' : 'disabled',
      reportFilename: 'bundle-report.html',
      openAnalyzer: false,
    }),
  ],
  module: {
    rules: [
      {
        test: /\.(ts|tsx)$/,
        use: [
          {
            loader: 'babel-loader',
            options: {
              presets: [
                '@babel/preset-env',
                ['@babel/preset-react', { runtime: 'automatic' }],
                '@babel/preset-typescript',
              ],
            },
          },
        ],
        exclude: /node_modules/,
      },
      {
        test: /\.css$/,
        use: ['style-loader', 'css-loader'],
      },
      {
        test: /\.(png|svg|jpg|jpeg|gif)$/i,
        type: 'asset',
      },
    ],
  },
  resolve: {
    extensions: ['.tsx', '.ts', '.js'],
  },
  performance: {
    maxAssetSize: 350 * 1024,
    maxEntrypointSize: 350 * 1024,
  },
  devServer: {
    static: {
      directory: path.join(__dirname, 'dist'),
    },
    port: 3000,
    open: false,
    hot: true,
    historyApiFallback: true,
    client: {
      overlay: true,
    },
  },
};
