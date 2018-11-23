import React, { Component } from 'react';
import { Button, Input } from 'bloomer';

class AddFolder extends Component {

    state = { folderName: null };

    updateFolderName = ({ target }) => this.setState({
        folderName: target.value
    });

    render() {
        return (
            <div>
                <Input
                    onChange={this.updateFolderName}
                    placeholder={'Folder Name'}
                />
                <Button
                    onClick={() => this.props.addFolder(this.state.folderName)}
                >
                    +
                </Button>
            </div>
        );
    }
}

export default AddFolder;