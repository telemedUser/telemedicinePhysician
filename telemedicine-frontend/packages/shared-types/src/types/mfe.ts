export interface MfeRemote {
  name: string;
  url: string;
  route: string;
  enabled: boolean;
  module?: string;
  scope?: string;
  roles?: string[];
  version?: string;
  minShellVersion?: string;
}

export interface MfeManifest {
  remotes: MfeRemote[];
  shellVersion?: string;
}
