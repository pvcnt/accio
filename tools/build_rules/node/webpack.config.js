/*
 * PDD is a platform for privacy-preserving Web searches collection.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
 *
 * PDD is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PDD is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PDD.  If not, see <http://www.gnu.org/licenses/>.
 */

const path = require('path');
const webpack = require('webpack');

const context = path.resolve(process.env.PWD, process.env._INPUT_DIR);
const output = path.resolve(process.env.PWD, process.env._OUTPUT_DIR);
const modules = process.env._PATH.split(',').map(dir => path.resolve(process.env.PWD, dir));
const isHeadless = process.env._HEADLESS === 'true';

const entry = {};
process.env._ENTRY.split(',').forEach(file => {
  const basename = path.relative(context, file);
  // Only .js files are allowed as entry points, per Bazel configuration.
  const name = basename.substr(0, basename.length - 3);
  entry[name] = './' + basename;
});

// '_' prefixed environment variables are private, i.e., not exported to the application.
const env = [] = Object.keys(process.env).filter(key => key[0] !== '_');

module.exports = {
  context: context,
  entry: entry,
  output: {
    path: output,
    filename: '[name].bundle.js',
  },
  mode: process.env.NODE_ENV === 'production' ? 'production' : 'development',
  target: isHeadless ? 'node' : 'web',
  module: {
    rules: [
      {
        test: /.jsx?$/,
        loader: 'babel-loader',
        exclude: /node_modules/,
        query: {
          presets: ['airbnb'],
          plugins: ['transform-decorators-legacy',],
        }
      },
      {
        test: /\.css$/,
        loader: isHeadless ? 'null-loader' : 'style-loader!css-loader',
      },
      {
        test: /\.(woff|woff2|eot|ttf|png|jpg|jpeg|svg)$/,
        loader: isHeadless ? 'null-loader' : 'url-loader',
      },
    ],
  },
  plugins: [
    new webpack.EnvironmentPlugin(env),
  ],
  resolveLoader: {
    modules: modules,
  },
  resolve: {
    modules: modules,
    extensions: ['.js', '.json', '.jsx'],
  },
};
