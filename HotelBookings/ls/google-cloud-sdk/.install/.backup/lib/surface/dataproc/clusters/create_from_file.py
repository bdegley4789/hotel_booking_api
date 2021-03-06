# Copyright 2018 Google Inc. All Rights Reserved.
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

"""Create a cluster from a file."""

from googlecloudsdk.api_lib.dataproc import dataproc as dp
from googlecloudsdk.api_lib.dataproc import util
from googlecloudsdk.calliope import base
from googlecloudsdk.command_lib.dataproc import clusters
from googlecloudsdk.command_lib.dataproc import flags


@base.ReleaseTracks(base.ReleaseTrack.BETA)
class CreateFromFile(base.CreateCommand):
  """Create a cluster from a file."""

  @staticmethod
  def Args(parser):
    parser.add_argument(
        '--file',
        help="""
        The path to a YAML file containing a Dataproc Cluster resource.

        For more information, see:
        https://cloud.google.com/dataproc/docs/reference/rest/v1beta2/projects.regions.clusters#Cluster.
        """,
        required=True)
    # TODO(b/80197067): Move defaults to a common location.
    flags.AddTimeoutFlag(parser, default='35m')
    base.ASYNC_FLAG.AddToParser(parser)

  def Run(self, args):
    dataproc = dp.Dataproc(self.ReleaseTrack())

    # Read cluster from YAML file.
    cluster = util.ReadYaml(args.file, dataproc.messages.Cluster)
    return clusters.CreateCluster(dataproc, cluster, args.async, args.timeout)
