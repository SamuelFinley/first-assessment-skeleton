import vorpal from 'vorpal'
import { words } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()

let username
let server
let host
let port
let pastCmd
let pastCnts

cli
  .delimiter(cli.chalk['yellow']('ftd~$'))

cli
  .mode('connect <username> [host] [port]')
  .delimiter(cli.chalk['green']('connected>'))
  .init(function (args, callback) {
    username = args.username
    if (!host) {
      host = 'localhost'
    } else {
      host = args.host
    }
    if (!port) {
      port = 8080
    } else {
      port = args.port
    }
    server = connect({ host: host, port: port }, () => {
      server.write(new Message({ username, command: 'connect' }).toJSON() + '\n')
      callback()
    })

    server.on('data', (buffer) => {
      this.log(Message.fromJSON(buffer).toString())
    })

    server.on('end', () => {
      cli.exec('exit')
    })
  })
  .action(function (input, callback) {
    const [ command, ...rest ] = words([input], /[@\w]+/g)
    const contents = rest.join(' ')
    if (command === 'disconnect') {
      server.end(new Message({ username, command }).toJSON() + '\n')
    } else if (command === 'echo') {
      pastCmd = command
      pastCnts = contents
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else if (command === 'broadcast') {
      pastCmd = command
      pastCnts = contents
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else if (command[0] === '@') {
      pastCmd = command
      pastCnts = contents
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else if (command === 'users') {
      pastCmd = command
      pastCnts = contents
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else if (input === '') {
      server.write(new Message({ username, command: pastCmd, contents: pastCnts }).toJSON() + '\n')
    } else {
      this.log(`Command <${command}> was not recognized`)
    }

    callback()
  })
