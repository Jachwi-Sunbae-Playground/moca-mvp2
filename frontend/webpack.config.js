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
                  // development 를 명시한다. webpack 의 --mode production 은 번들 안의 NODE_ENV 만 바꾸고
                  // 빌드 프로세스의 NODE_ENV 는 건드리지 않는다. babel 이 그걸 기본값 development 로 보고
                  // jsxDEV 를 내보내면, React 19 의 운영 런타임에 그 함수가 없어 화면이 뜨지 않는다.
                  ['@babel/preset-react', { runtime: 'automatic', development: !isProduction }],
                  '@babel/preset-typescript',
                ],
              },
            },
          ],
          exclude: /node_modules/,
        },
        {
          test: /\.css$/,
          use: [
            'style-loader',
            {
              loader: 'css-loader',
              options: {
                modules: {
                  auto: /\.module\.css$/i,
                  namedExport: false,
                  localIdentName: isProduction ? '[hash:base64:6]' : '[name]__[local]__[hash:base64:5]',
                },
              },
            },
          ],
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
