# Copyright 2016 Google Inc. All Rights Reserved.
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
"""Functions for creating GCE container (Docker) deployments."""
from __future__ import absolute_import
from __future__ import unicode_literals
import itertools
import re
import enum

from googlecloudsdk.api_lib.compute import metadata_utils
from googlecloudsdk.calliope import exceptions
from googlecloudsdk.core import exceptions as core_exceptions
from googlecloudsdk.core import yaml
from googlecloudsdk.core.util import times
import six

USER_INIT_TEMPLATE = """#cloud-config
runcmd:
- ['/usr/bin/kubelet',
   '--allow-privileged=%s',
   '--manifest-url=http://metadata.google.internal/computeMetadata/v1/instance/attributes/google-container-manifest',
   '--manifest-url-header=Metadata-Flavor:Google',
   '--config=/etc/kubernetes/manifests']
"""

MANIFEST_DISCLAIMER = """# DISCLAIMER:
# This container declaration format is not a public API and may change without
# notice. Please use gcloud command-line tool or Google Cloud Console to run
# Containers on Google Compute Engine.

"""

USER_DATA_KEY = 'user-data'

CONTAINER_MANIFEST_KEY = 'google-container-manifest'

GCE_CONTAINER_DECLARATION = 'gce-container-declaration'

GKE_DOCKER = 'gci-ensure-gke-docker'

ALLOWED_PROTOCOLS = ['TCP', 'UDP']

# Prefix of all COS image major release names
COS_MAJOR_RELEASE_PREFIX = 'cos-stable-'

# Pin this version of gcloud to COS image major release version
COS_MAJOR_RELEASE = COS_MAJOR_RELEASE_PREFIX + '55'

COS_PROJECT = 'cos-cloud'

_MIN_PREFERRED_COS_VERSION = 63

# Translation from CLI to API wording
RESTART_POLICY_API = {
    'never': 'Never',
    'on-failure': 'OnFailure',
    'always': 'Always'
}


def _GetUserInit(allow_privileged):
  """Gets user-init metadata value for COS image."""
  allow_privileged_val = 'true' if allow_privileged else 'false'
  return USER_INIT_TEMPLATE % (allow_privileged_val)


class InvalidMetadataKeyException(exceptions.ToolException):
  """InvalidMetadataKeyException is for not allowed metadata keys."""

  def __init__(self, metadata_key):
    super(InvalidMetadataKeyException, self).__init__(
        'Metadata key "{0}" is not allowed when running containerized VM.'
        .format(metadata_key))


class NoGceContainerDeclarationMetadataKey(core_exceptions.Error):
  """Raised on attempt to update-container on instance without containers."""

  def __init__(self):
    super(NoGceContainerDeclarationMetadataKey, self).__init__(
        "Instance doesn't have {} metadata key - it is not a container.".format(
            GCE_CONTAINER_DECLARATION))


def ValidateUserMetadata(metadata):
  """Validates if user-specified metadata.

  Checks if it contains values which may conflict with container deployment.
  Args:
    metadata: user-specified VM metadata.

  Raises:
    InvalidMetadataKeyException: if there is conflict with user-provided
    metadata
  """
  for entry in metadata.items:
    if entry.key in [USER_DATA_KEY, CONTAINER_MANIFEST_KEY, GKE_DOCKER]:
      raise InvalidMetadataKeyException(entry.key)


def CreateTagsMessage(messages, tags):
  """Create tags message with parameters for container VM or VM templates."""
  if tags:
    return messages.Tags(items=tags)


def GetLabelsMessageWithCosVersion(
    labels, image_uri, resources, resource_class):
  """Returns message with labels for instance / instance template.

  Args:
    labels: dict, labels to assign to the resource.
    image_uri: URI of image used as a base for the resource. The function
               extracts COS version from the URI and uses it as a value of
               `container-vm` label.
    resources: object that can parse image_uri.
    resource_class: class of the resource to which labels will be assigned.
                    Must contain LabelsValue class and
                    resource_class.LabelsValue must contain AdditionalProperty
                    class.
  """
  cos_version = resources.Parse(
      image_uri, collection='compute.images').Name().replace('/', '-')
  if labels is None:
    labels = {}
  labels['container-vm'] = cos_version
  additional_properties = [
      resource_class.LabelsValue.AdditionalProperty(key=k, value=v)
      for k, v in sorted(six.iteritems(labels))]
  return resource_class.LabelsValue(additionalProperties=additional_properties)


class NoCosImageException(core_exceptions.Error):
  """Raised when COS image could not be found."""

  def __init__(self):
    super(NoCosImageException, self).__init__(
        'Could not find COS (Cloud OS) for release family \'{0}\''
        .format(COS_MAJOR_RELEASE))


def ExpandCosImageFlag(compute_client):
  """Select a COS image to run Docker."""
  compute = compute_client.apitools_client
  images = compute_client.MakeRequests([(
      compute.images,
      'List',
      compute_client.messages.ComputeImagesListRequest(project=COS_PROJECT)
  )])
  return _SelectNewestCosImage(images)


def _SelectNewestCosImage(images):
  """Selects newest COS image from the list."""
  cos_images = sorted([image for image in images
                       if image.name.startswith(COS_MAJOR_RELEASE)],
                      key=lambda x: times.ParseDateTime(x.creationTimestamp))
  if not cos_images:
    raise NoCosImageException()
  return cos_images[-1].selfLink


def _ValidateAndParsePortMapping(port_mappings):
  """Parses and validates port mapping."""
  ports_config = []
  for port_mapping in port_mappings:
    mapping_match = re.match(r'^(\d+):(\d+):(\S+)$', port_mapping)
    if not mapping_match:
      raise exceptions.InvalidArgumentException(
          '--port-mappings',
          'Port mappings should follow PORT:TARGET_PORT:PROTOCOL format.')
    port, target_port, protocol = mapping_match.groups()
    if protocol not in ALLOWED_PROTOCOLS:
      raise exceptions.InvalidArgumentException(
          '--port-mappings',
          'Protocol should be one of [{0}]'.format(
              ', '.join(ALLOWED_PROTOCOLS)))
    ports_config.append({
        'containerPort': int(target_port),
        'hostPort': int(port),
        'protocol': protocol})
  return ports_config


class MountVolumeMode(enum.Enum):
  READ_ONLY = 1,
  READ_WRITE = 2,

  def isReadOnly(self):
    return self == MountVolumeMode.READ_ONLY


def ExpandKonletCosImageFlag(compute_client):
  """Select a COS image to run Konlet.

  This function scans three families in order:
  - stable
  - beta
  - dev
  looking for the first image with version at least _MIN_PREFERRED_COS_VERSION.

  Args:
    compute_client: ClientAdapter, The Compute API client adapter

  Returns:
    COS image at version _MIN_PREFERRED_COS_VERSION or later.

  Raises:
    NoCosImageException: No COS image at version at least
    _MIN_PREFERRED_COS_VERSION was found. This should not happen if backend is
    healthy.
  """
  compute = compute_client.apitools_client
  images = compute_client.MakeRequests(
      [(compute.images,
        'List',
        compute_client.messages.ComputeImagesListRequest(project=COS_PROJECT))])
  name_re_template = r'cos-{}-(\d+)-.*'
  image_families = ['stable', 'beta', 'dev']

  for family in image_families:
    name_re = name_re_template.format(family)
    def MakeCreateComparisonKey(name_re):
      def CreateComparisonKey(image):
        version = int(re.match(name_re, image.name).group(1))
        timestamp = times.ParseDateTime(image.creationTimestamp)
        return version, timestamp
      return CreateComparisonKey

    cos_images = sorted(
        [image for image in images if re.match(name_re, image.name)],
        key=MakeCreateComparisonKey(name_re))
    if (cos_images and MakeCreateComparisonKey(name_re)(cos_images[-1])[0] >=
        _MIN_PREFERRED_COS_VERSION):
      return cos_images[-1].selfLink

  raise NoCosImageException()


def _ReadDictionary(filename):
  # pylint:disable=line-too-long
  r"""Read environment variable from file.

  File format:

  It is intended (but not guaranteed) to follow standard docker format
  [](https://docs.docker.com/engine/reference/commandline/run/#set-environment-variables--e---env---env-file)
  but without capturing environment variables from host machine.
  Lines starting by "#" character are comments.
  Empty lines are ignored.
  Below grammar production follow in EBNF format.

  file = (whitespace* statement '\n')*
  statement = comment
            | definition
  whitespace = ' '
             | '\t'
  comment = '#' [^\n]*
  definition = [^#=\n] [^= \t\n]* '=' [^\n]*

  Args:
    filename: str, name of the file to read

  Returns:
    A dictionary mapping environment variable names to their values.
  """
  env_vars = {}
  if not filename:
    return env_vars
  try:
    with open(filename, 'r') as f:
      for i, line in enumerate(f):
        # Strip whitespace at the beginning and end of line
        line = line.strip()
        # Ignore comments and empty lines
        if len(line) <= 1 or line[0] == '#':
          continue
        # Find first left '=' character
        assignment_op_loc = line.find('=')
        if assignment_op_loc == -1:
          raise exceptions.BadFileException(
              'Syntax error in {}:{}: Expected VAR=VAL, got {}'.format(
                  filename, i, line))
        env = line[:assignment_op_loc]
        val = line[assignment_op_loc+1:]
        if ' ' in env or '\t' in env:
          raise exceptions.BadFileException(
              'Syntax error in {}:{} Variable name cannot contain whitespaces,'
              ' got "{}"'.format(filename, i, env))
        env_vars[env] = val
  except IOError as e:
    raise exceptions.BadFileException(e)
  return env_vars


def _GetHostPathDiskName(idx):
  return 'host-path-{}'.format(idx)


def _GetTmpfsDiskName(idx):
  return 'tmpfs-{}'.format(idx)


def _CreateContainerManifest(args, instance_name):
  """Create container manifest from argument namespace and instance name."""
  container = {'image': args.container_image, 'name': instance_name}

  if args.container_command is not None:
    container['command'] = [args.container_command]

  if args.container_arg is not None:
    container['args'] = args.container_arg

  container['stdin'] = args.container_stdin
  container['tty'] = args.container_tty
  container['securityContext'] = {'privileged': args.container_privileged}

  env_vars = _ReadDictionary(args.container_env_file)
  for env_var_dict in args.container_env or []:
    for env, val in six.iteritems(env_var_dict):
      env_vars[env] = val
  if env_vars:
    container['env'] = [{
        'name': env,
        'value': val
    } for env, val in six.iteritems(env_vars)]

  volumes = []
  volume_mounts = []
  default_mode = MountVolumeMode.READ_WRITE
  for idx, volume in enumerate(args.container_mount_host_path or []):
    volumes.append({
        'name': _GetHostPathDiskName(idx),
        'hostPath': {
            'path': volume['host-path']
        },
    })
    volume_mounts.append({
        'name': _GetHostPathDiskName(idx),
        'mountPath': volume['mount-path'],
        'readOnly': volume.get('mode', default_mode).isReadOnly()
    })
  for idx, tmpfs in enumerate(args.container_mount_tmpfs or []):
    volumes.append(
        {'name': _GetTmpfsDiskName(idx), 'emptyDir': {'medium': 'Memory'}})
    volume_mounts.append(
        {'name': _GetTmpfsDiskName(idx), 'mountPath': tmpfs['mount-path']})

  container['volumeMounts'] = volume_mounts

  manifest = {
      'spec': {
          'containers': [container],
          'volumes': volumes,
          'restartPolicy': RESTART_POLICY_API[args.container_restart_policy]
      }
  }

  return manifest


def DumpYaml(data):
  """Dumps data dict to YAML in format expected by Konlet."""
  return MANIFEST_DISCLAIMER + yaml.dump(data)


def _CreateYamlContainerManifest(args, instance_name):
  return DumpYaml(_CreateContainerManifest(args, instance_name))


def CreateKonletMetadataMessage(messages, args, instance_name, user_metadata):
  konlet_metadata = {
      GCE_CONTAINER_DECLARATION:
          _CreateYamlContainerManifest(args, instance_name)
  }
  return metadata_utils.ConstructMetadataMessage(
      messages, metadata=konlet_metadata, existing_metadata=user_metadata)


def UpdateMetadata(metadata, args):
  """Update konlet metadata entry using user-supplied data."""
  # precondition: metadata.key == GCE_CONTAINER_DECLARATION

  manifest = yaml.load(metadata.value)

  if args.IsSpecified('container_image'):
    manifest['spec']['containers'][0]['image'] = args.container_image

  if args.IsSpecified('container_command'):
    manifest['spec']['containers'][0]['command'] = [args.container_command]

  if args.IsSpecified('clear_container_command'):
    manifest['spec']['containers'][0].pop('command', None)

  if args.IsSpecified('container_arg'):
    manifest['spec']['containers'][0]['args'] = args.container_arg

  if args.IsSpecified('clear_container_args'):
    manifest['spec']['containers'][0].pop('args', None)

  if args.container_privileged is True:
    manifest['spec']['containers'][0]['securityContext']['privileged'] = True

  if args.container_privileged is False:
    manifest['spec']['containers'][0]['securityContext']['privileged'] = False

  _UpdateMounts(manifest, args.remove_container_mounts or [],
                args.container_mount_host_path or [],
                args.container_mount_tmpfs or [])

  _UpdateEnv(manifest,
             itertools.chain.from_iterable(args.remove_container_env or []),
             args.container_env_file, args.container_env or [])

  if args.container_stdin is True:
    manifest['spec']['containers'][0]['stdin'] = True

  if args.container_stdin is False:
    manifest['spec']['containers'][0]['stdin'] = False

  if args.container_tty is True:
    manifest['spec']['containers'][0]['tty'] = True

  if args.container_tty is False:
    manifest['spec']['containers'][0]['tty'] = False

  if args.IsSpecified('container_restart_policy'):
    manifest['spec']['restartPolicy'] = RESTART_POLICY_API[
        args.container_restart_policy]

  metadata.value = DumpYaml(manifest)


def _UpdateMounts(manifest, remove_container_mounts, container_mount_host_path,
                  container_mount_tmpfs):
  """Updates mounts in container manifest."""

  _CleanupMounts(manifest, remove_container_mounts, container_mount_host_path,
                 container_mount_tmpfs)

  used_names = [volume['name'] for volume in manifest['spec']['volumes']]
  volumes = []
  volume_mounts = []
  next_volume_index = 0
  default_mode = MountVolumeMode.READ_WRITE
  for volume in container_mount_host_path:
    while _GetHostPathDiskName(next_volume_index) in used_names:
      next_volume_index += 1
    name = _GetHostPathDiskName(next_volume_index)
    next_volume_index += 1
    volumes.append({
        'name': name,
        'hostPath': {
            'path': volume['host-path']
        },
    })
    volume_mounts.append({
        'name': name,
        'mountPath': volume['mount-path'],
        'readOnly': volume.get('mode', default_mode).isReadOnly()
    })
  for tmpfs in container_mount_tmpfs:
    while _GetTmpfsDiskName(next_volume_index) in used_names:
      next_volume_index += 1
    name = _GetTmpfsDiskName(next_volume_index)
    next_volume_index += 1
    volumes.append({'name': name, 'emptyDir': {'medium': 'Memory'}})
    volume_mounts.append({'name': name, 'mountPath': tmpfs['mount-path']})

  manifest['spec']['containers'][0]['volumeMounts'].extend(volume_mounts)
  manifest['spec']['volumes'].extend(volumes)


def _CleanupMounts(manifest, remove_container_mounts, container_mount_host_path,
                   container_mount_tmpfs):
  """Remove all specified mounts from container manifest."""

  # valumeMounts stored in this list should be removed
  mount_paths_to_remove = remove_container_mounts[:]
  for host_path in container_mount_host_path:
    mount_paths_to_remove.append(host_path['mount-path'])
  for tmpfs in container_mount_tmpfs:
    mount_paths_to_remove.append(tmpfs['mount-path'])

  # volumeMounts stored in this list are used
  used_mounts = []
  used_mounts_names = []
  removed_mount_names = []
  for mount in manifest['spec']['containers'][0].get('volumeMounts', []):
    if mount['mountPath'] not in mount_paths_to_remove:
      used_mounts.append(mount)
      used_mounts_names.append(mount['name'])
    else:
      removed_mount_names.append(mount['name'])

  # override volumeMounts
  manifest['spec']['containers'][0]['volumeMounts'] = used_mounts
  # garbage collect volumes which become orphaned, skip volumes orphaned before
  # start of the procedure
  used_volumes = []
  for volume in manifest['spec'].get('volumes', []):
    if (volume['name'] in used_mounts_names or
        volume['name'] not in removed_mount_names):
      used_volumes.append(volume)

  # override volumes
  manifest['spec']['volumes'] = used_volumes


def _UpdateEnv(manifest, remove_container_env, container_env_file,
               container_env):
  """Update environment variables in container manifest."""

  current_env = {}
  for env_val in manifest['spec']['containers'][0].get('env', []):
    current_env[env_val['name']] = env_val['value']

  for env in remove_container_env:
    current_env.pop(env, None)

  current_env.update(_ReadDictionary(container_env_file))

  for env_var_dict in container_env:
    for env, val in six.iteritems(env_var_dict):
      current_env[env] = val
  if current_env:
    manifest['spec']['containers'][0]['env'] = [{
        'name': env,
        'value': val
    } for env, val in six.iteritems(current_env)]
