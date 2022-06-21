# Updating to a newer Anki version

- Take the changes in the current ankidroid-xxx branch in the rslib-bridge/anki
  repo, and rebase them over the latest desktop release.
- Use the GitHub action in that repo to generate a new release artifact, which includes
  build hash and web files.
- Update artifactZipLocation in rsdroid/build.grade to point to the new artifact
- Test things build locally with ./build-current.sh
