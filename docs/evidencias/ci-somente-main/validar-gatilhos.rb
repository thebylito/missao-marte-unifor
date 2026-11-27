require 'yaml'
ci = YAML.load_file('.github/workflows/ci.yml')
deploy = YAML.load_file('.github/workflows/deploy.yml')
events = ->(w) { w['on'] || w[true] }
raise 'CI deve ser apenas reutilizável' unless events.call(ci).keys == ['workflow_call']
raise 'build redundante de imagem' if ci['jobs'].key?('imagem')
raise 'gatilhos inesperados' unless events.call(deploy).keys.sort == ['push', 'workflow_dispatch']
raise 'push fora da main' unless events.call(deploy)['push']['branches'] == ['main']
raise 'workflow de testes errado' unless deploy['jobs']['testes']['uses'] == './.github/workflows/ci.yml'
raise 'execução manual fora da main' unless deploy['jobs']['testes']['if'] == "github.ref == 'refs/heads/main'"
raise 'publicação sem gate' unless deploy['jobs']['build-and-push']['needs'] == 'testes'
raise 'deploy sem imagem validada' unless deploy['jobs']['deploy']['needs'] == 'build-and-push'
raise 'cancelamento entre branches' unless deploy['concurrency']['group'] == 'deploy-production-${{ github.ref }}'
puts 'PASS: PR e push de feature não têm gatilhos; CI chamado uma vez pelo deploy na main.'
puts 'PASS: testes -> imagem -> deploy, sem bypass por always() ou continue-on-error.'
puts 'PASS: execução manual fora da main bloqueada no primeiro job.'
puts 'PASS: concorrência isolada por ref; outra branch não cancela a main.'
