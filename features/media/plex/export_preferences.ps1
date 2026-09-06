<#
.SYNOPSIS
    Convertit les preferences Plex du registre Windows en Preferences.xml Linux.

.DESCRIPTION
    C'est LA piece que la plupart des guides de migration oublient : sous Windows,
    Plex Media Server ne stocke pas ses reglages dans Preferences.xml mais dans
    HKCU\Software\Plex, Inc.\Plex Media Server. Sans ce fichier, le conteneur
    demarre en serveur NEUF : nouvelle identite machine, les comptes partages
    perdent le serveur, l'acces distant est a refaire.

    Valeurs vitales reportees : MachineIdentifier, ProcessedMachineIdentifier,
    AnonymousMachineIdentifier, CertificateUUID, PlexOnlineToken.

.PARAMETER Destination
    Chemin du Preferences.xml a ecrire. Doit pointer dans la copie de travail,
    typiquement :
      <staging>\Plex Media Server\Preferences.xml

.NOTES
    /!\ Le fichier produit contient le PlexOnlineToken : c'est un SECRET.
        Ne jamais le committer, ne jamais le coller dans une conversation.
        L'ecrire hors du repo (le repo ignore de toute facon ce chemin).
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$Destination
)

$ErrorActionPreference = 'Stop'

$key = 'HKCU:\Software\Plex, Inc.\Plex Media Server'
if (-not (Test-Path $key)) {
    throw "Cle de registre introuvable : $key — Plex natif est-il bien installe sous cet utilisateur ?"
}

# Valeurs sans lesquelles le serveur perd son identite : on echoue plutot que de
# produire un Preferences.xml qui donnerait un serveur neuf.
$requises = @(
    'MachineIdentifier',
    'ProcessedMachineIdentifier',
    'PlexOnlineToken',
    'PlexOnlineUsername'
)

$props = (Get-ItemProperty $key).PSObject.Properties |
    Where-Object { $_.Name -notmatch '^PS(Path|ParentPath|ChildName|Drive|Provider)$' } |
    # Les `_<hexa>-TranscodeCountLimit` sont des reglages par client, sans interet ici.
    Where-Object { $_.Name -notlike '_*' }

$noms = $props | Select-Object -ExpandProperty Name
$manquantes = $requises | Where-Object { $_ -notin $noms }
if ($manquantes) {
    throw "Valeurs vitales absentes du registre : $($manquantes -join ', ') — migration a ne PAS poursuivre."
}

$dossier = Split-Path -Parent $Destination
if (-not (Test-Path $dossier)) {
    throw "Le dossier de destination n'existe pas : $dossier"
}

# XmlWriter fait l'echappement des attributs (& < > ") correctement.
$settings = New-Object System.Xml.XmlWriterSettings
$settings.Indent = $true
$settings.Encoding = New-Object System.Text.UTF8Encoding($false)

$writer = [System.Xml.XmlWriter]::Create($Destination, $settings)
try {
    $writer.WriteStartDocument()
    $writer.WriteStartElement('Preferences')
    foreach ($p in ($props | Sort-Object Name)) {
        # Les DWORD reviennent en Int32 ; Plex attend des chaines ("1", "0", ...).
        $writer.WriteAttributeString($p.Name, [string]$p.Value)
    }
    $writer.WriteEndElement()
    $writer.WriteEndDocument()
}
finally {
    $writer.Flush()
    $writer.Close()
}

# On confirme le report de l'identite SANS afficher la moindre valeur sensible.
$xml = [xml](Get-Content $Destination)
$ecrites = $xml.Preferences.Attributes.Count
$identiteOk = -not [string]::IsNullOrWhiteSpace($xml.Preferences.GetAttribute('MachineIdentifier'))

Write-Host "Preferences.xml ecrit : $Destination"
Write-Host "  attributs reportes  : $ecrites"
Write-Host "  MachineIdentifier   : $(if ($identiteOk) { 'present (valeur masquee)' } else { 'ABSENT — STOP' })"
Write-Host "  /!\ contient le PlexOnlineToken : ne pas committer, ne pas afficher."

if (-not $identiteOk) { exit 1 }
