const path = require('path');
const webpack = require('webpack');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const { BundleAnalyzerPlugin } = require('webpack-bundle-analyzer');

const shouldAnalyze = process.env.ANALYZE === 'true';
const isBrowserTestHarness = process.env.BROWSER_TEST_HARNESS === 'true';

module.exports = (_env, argv) => {
  const isProduction = argv.mode === 'production';

  return {
    entry: isBrowserTestHarness ? './src/test-browser/main.tsx' : './src/main.tsx',
    output: {
      path: path.resolve(__dirname, 'dist'),
      // 운영에서만 contenthash 를 붙인다. 내용이 바뀌면 파일명이 바뀌므로 CDN 캐시를 무효화하지 않아도
      // 브라우저가 새 파일을 받는다. 개발에서는 파일명이 매번 바뀌면 dev-server 의 HMR 이 불편해진다.
      filename: isProduction ? '[name].[contenthash].js' : '[name].js',
      chunkFilename: isProduction ? '[name].[contenthash].js' : '[name].js',
      assetModuleFilename: isProduction ? 'assets/[name].[contenthash][ext]' : 'assets/[name][ext]',
      publicPath: '/',
      clean: true,
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
};
