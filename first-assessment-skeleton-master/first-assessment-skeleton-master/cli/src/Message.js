import { cli } from './cli'
export class Message {
  static fromJSON (buffer) {
    let msg = new Message(JSON.parse(buffer.toString()))
    let message = ''
    if (msg.command === 'connect') {
      let stringMess = msg.time + ' <' + msg.username + '> has connected'
      message = cli.chalk['green'](stringMess)
    } else if (msg.command === 'disconnect') {
      let stringMess = msg.time + ' <' + msg.contents + '> has disconnected'
      message = cli.chalk['red'](stringMess)
    } else if (msg.command === 'echo') {
      let stringMess = msg.time + ' <' + msg.username + '> (echo): ' + msg.contents
      message = cli.chalk['magenta'](stringMess)
    } else if (msg.command === 'broadcast') {
      let stringMess = msg.time + ' <' + msg.username + '> (all): ' + msg.contents
      message = cli.chalk['yellow'](stringMess)
    } else if (msg.command === '@') {
      let stringMess = msg.time + ' <' + msg.username + '> (whisper): ' + msg.contents
      message = cli.chalk['blue'](stringMess)
    } else if (msg.command === 'users') {
      let names = ''
      for (let name of (msg.contents).split(':')) {
        names += '<' + name + '> '
      }
      let stringMess = msg.time + ': currently connected users:\n' + names
      message = cli.chalk['cyan'](stringMess)
    } else if (msg.command === 'username error') {
      message = cli.chalk['red']('This username is already in use. Please pick another')
    }
    return message
  }

  constructor ({ username, command, contents, time }) {
    this.username = username
    this.command = command
    this.contents = contents
    this.time = time
  }

  toJSON () {
    return JSON.stringify({
      username: this.username,
      command: this.command,
      contents: this.contents,
      time: this.time
    })
  }

  toString () {
    return this.contents
  }
}
