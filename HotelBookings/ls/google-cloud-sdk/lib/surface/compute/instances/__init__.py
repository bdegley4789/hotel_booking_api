# Copyright 2014 Google Inc. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Commands for reading and manipulating instances."""

from __future__ import absolute_import
from __future__ import unicode_literals
from googlecloudsdk.calliope import base


@base.CommandSuggestion('connect-to-serial-port',
                        'compute connect-to-serial-port')
@base.CommandSuggestion('copy-files', 'compute copy-files')
@base.CommandSuggestion('reset-windows-password',
                        'compute reset-windows-password')
@base.CommandSuggestion('scp', 'compute scp')
@base.CommandSuggestion('ssh', 'compute ssh')
class Instances(base.Group):
  """Read and manipulate Google Compute Engine virtual machine instances."""


Instances.detailed_help = {
    'brief': (
        'Read and manipulate Google Compute Engine virtual machine instances'),
}
