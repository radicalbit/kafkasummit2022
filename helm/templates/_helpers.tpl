{{/*
Expand the name of the chart.
*/}}
{{- define "kafkasummit2022.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "kafkasummit2022.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "kafkasummit2022.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "kafkasummit2022.labels" -}}
kafkasummit2022.sh/chart: {{ include "kafkasummit2022.chart" . }}
{{ include "kafkasummit2022.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- if .Values.labels }}
{{ toYaml .Values.labels }}
{{- end }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "kafkasummit2022.selectorLabels" -}}
app.kubernetes.io/name: {{ include "kafkasummit2022.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- if .Values.labels }}
{{ toYaml .Values.labels }}
{{- end }}
{{- end }}


{{/*
Create the name of the cluster role to use
*/}}
{{- define "kafkasummit2022.clusterRoleName" -}}
{{- if .Values.clusterRole.create }}
{{- default (include "kafkasummit2022.fullname" .) .Values.clusterRole.name }}
{{- else }}
{{- default "default" .Values.clusterRole.name }}
{{- end }}
{{- end }}

{{/*
Create the name of the role binding to use
*/}}
{{- define "kafkasummit2022.roleBindingName" -}}
{{- if and (.Values.serviceAccount.create) (.Values.clusterRole.create) -}}
{{- default (include "kafkasummit2022.fullname" .) .Values.roleBinding.name }}
{{- else }}
{{- default "default" .Values.roleBinding.name }}
{{- end }}
{{- end }}
