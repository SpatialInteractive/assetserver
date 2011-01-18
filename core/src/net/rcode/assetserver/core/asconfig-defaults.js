/**
 * This file is loaded just as an .asconfig file prior to
 * any other configs.
 */
filter.on('*.js', '*.css', '*.html', 'ejs');
filter.on('*.js', 'jsoptimize');
filter.on('*.css', 'cssoptimize');

