# -*- coding: utf-8 -*-
import sys, os

project = u'ChannelFinder Service'
copyright = u'Copyright (c) 2010-2020 Brookhaven National Laboratory \n Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH \n All rights reserved. Use is subject to license terms and conditions.'
version = '1.0'
release = '1.0.0'

# General options
needs_sphinx = '1.0'
master_doc = 'index'
pygments_style = 'tango'
add_function_parentheses = True

extensions = ['sphinx.ext.autodoc' ]

templates_path = ['_templates']
exclude_trees = ['.build']
source_suffix = ['.rst', '.md']
source_encoding = 'utf-8-sig'

# HTML options
html_theme = 'sphinx_rtd_theme'
html_short_title = "my-project"
htmlhelp_basename = 'my-project-doc'
html_use_index = True
html_show_sourcelink = False
html_static_path = ['_static']

# PlantUML options
plantuml = os.getenv('plantuml')
