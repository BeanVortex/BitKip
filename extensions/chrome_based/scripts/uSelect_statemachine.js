/*
 * statemachine.js
 * A simple state machine implementation for the Javascript programming
 * language.
 *
 * Copyright © 2011 Alessandro Guido
 * Copyright © 2011 Marco Palumbo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * StateMachine constructor.
 *
 * @constructor
 */
function StateMachine() {
	/**
	 * Collection of possible states.
	 *
	 * @public
	 * @type {Object[]}
	 */
	this.states = {};

	/**
	 * Easily access the current state object.
	 *
	 * There are 2 special values:
	 * - undefined: the state machine has not been started
	 * - null: the state machine has terminated
	 *
	 * @private
	 * @type {Object}
	 */
	this._current = undefined;
}

/**
 * Handlers in the form "__*__" have a special meaning and
 * cannot be fired with StateMachine.fireEvent()
 *
 * @private
 */
StateMachine._restricted_event = /^__.*__$/;

/**
 * Start the state machine.
 *
 * @this {StateMachine}
 * @param {string} initialState Name of initial state.
 */
StateMachine.prototype.start = function (initialState) {
	if (this._current !== undefined)
		throw 'State machine already started';
	if (initialState === undefined || initialState === null)
		throw 'Please give a valid state name';

	setTimeout(StateMachine._changeState, 0, this, initialState);
};

/**
 * Signal that an event has occurred.
 *
 * @this {StateMachine}
 * @param {string} name Event name.
 * @param arg Event argument (optional).
 * @throws If state machine is not running or when firing a restricted event.
 */
StateMachine.prototype.fireEvent = function (name, arg) {
	if (this._current === undefined)
		throw 'State machine not started';
	if (this._current === null)
		throw 'State machine terminated';
	if (StateMachine._restricted_event.test(name))
		throw 'Firing event "' + name + '" is not allowed';

	setTimeout(StateMachine._handleEvent, 0, this, name, arg);
};

/**
 * Exit from the current state (if defined) and enter the next one.
 * Should be called with asynchronously.
 *
 * @private
 * @param {StateMachine} self StateMachine instance.
 * @param {string|null} newstate Name of the state.
 */
StateMachine._changeState = function (self, newstate) {
	if (newstate !== null && !self.states.hasOwnProperty(newstate)) {
		self._current = null;
		throw 'No such state "' + newstate + '"';
	}

	var curr = self._current;

	if (curr !== undefined && curr.hasOwnProperty('__exit__')) {
		/* this._current is undefined only when the state machine has
		 * just been started */
		curr.__exit__.apply(self);
	}

	if (newstate === null) {
		/* state machine will terminate */
		self._current = null;
		return;
	}

	curr = self._current = self.states[newstate];

	if (!curr.hasOwnProperty('__enter__'))
		return;

	var next = curr.__enter__.apply(self);
	if (next !== undefined)
		setTimeout(StateMachine._changeState, 0, self, next);
};

/**
 * Execute the event handler and if needed schedule a state change.
 * Should be called asynchronously.
 *
 * @private
 * @param {StateMachine} self StateMachine instance.
 * @param {string} name Name of event.
 * @param arg Event argument.
 */
StateMachine._handleEvent = function (self, name, arg) {
	var next = self._current[name];

	if (typeof next == 'function')
		next = next.apply(self, arg);

	if (next === undefined) // state unchanged
		return;

	setTimeout(StateMachine._changeState, 0, self, next);
};
